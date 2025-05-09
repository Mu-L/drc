package com.ctrip.framework.drc.console.aop.forward;

import com.ctrip.framework.drc.console.config.DefaultConsoleConfig;
import com.ctrip.framework.drc.console.enums.ForwardTypeEnum;
import com.ctrip.framework.drc.console.enums.HttpRequestEnum;
import com.ctrip.framework.drc.console.service.v2.CentralService;
import com.ctrip.framework.drc.core.http.ApiResult;
import com.ctrip.framework.drc.core.http.HttpUtils;
import com.ctrip.framework.drc.core.service.utils.JsonUtils;
import com.ctrip.xpipe.utils.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @ClassName RemoteAspect
 * @Author haodongPan
 * @Date 2022/6/21 16:57
 * @Version: $
 */
@Component
@Aspect
public class RemoteHttpAspect {

    private static final Logger logger = LoggerFactory.getLogger(RemoteHttpAspect.class);

    @Autowired
    private DefaultConsoleConfig consoleConfig;

    @Autowired
    private CentralService centralService;

    private static final Map<String, String> mha2DcMap = Maps.newConcurrentMap();

    @Pointcut("within(com.ctrip.framework.drc.console.service..*) && " +
            "@annotation(com.ctrip.framework.drc.console.aop.forward.PossibleRemote)")
    public void pointCut(){};

    @Around(value = "pointCut() && @annotation(possibleRemote)")
    public Object aroundOperate(ProceedingJoinPoint point, PossibleRemote possibleRemote) {
        try {
            String localRegion = consoleConfig.getRegion();
            String centerRegion = consoleConfig.getCenterRegion();

            Map<String, String> consoleRegionUrls = consoleConfig.getConsoleRegionUrls();

            Map<String, Object> argsNotExcluded = getArgsNotExcluded(point,possibleRemote.excludeArguments());
            ForwardTypeEnum forwardType = possibleRemote.forwardType();

            switch (forwardType) {
                case TO_META_DB:
                    if (centerRegion.equals(localRegion)) {
                        return invokeOriginalMethod(point);
                    } else {
                        StringBuilder url = new StringBuilder(consoleConfig.getCenterRegionUrl());
                        return forwardByHttp(url,possibleRemote,argsNotExcluded);
                    }
                case TO_OVERSEA_BY_ARG:
                    String regionByArgs = getRegionByArgs(argsNotExcluded, possibleRemote);
                    if (localRegion.equals(regionByArgs)) {
                        return invokeOriginalMethod(point);
                    } else {
                        StringBuilder url = new StringBuilder(consoleRegionUrls.get(regionByArgs));
                        if (StringUtils.isEmpty(url)) {
                            throw new IllegalArgumentException("no regionUrl find, region: " + regionByArgs);
                        }
                        return forwardByHttp(url,possibleRemote,argsNotExcluded);
                    }
                default:
                    return invokeOriginalMethod(point);
            }
        } catch (Throwable e) {
            logger.error("[[tag=remoteHttpAop]] error", e);
            return null;
        }
    }

    private Object invokeOriginalMethod(ProceedingJoinPoint point) throws Throwable{
        Object[] args = point.getArgs();
        return point.proceed(args);
    }

    private Object forwardByHttp(StringBuilder regionUrl, PossibleRemote possibleRemote, Map<String, Object> args) {
        regionUrl.append(possibleRemote.path());
        ApiResult apiResult;
        HttpRequestEnum httpRequestType = possibleRemote.httpType();
        switch (httpRequestType) {
            case GET:
                spliceUrl(regionUrl, args);
                apiResult = HttpUtils.get(regionUrl.toString(), possibleRemote.responseType());
                break;
            case PUT:
                apiResult = HttpUtils.put(regionUrl.toString(), possibleRemote.responseType());
                break;
            case POST:
                String paramValue = JsonUtils.toJson(args.get("requestBody"));
                Object requestBody = JsonUtils.fromJson(paramValue, possibleRemote.requestClass());
                apiResult = HttpUtils.post(regionUrl.toString(), requestBody, possibleRemote.responseType());
                break;
            case DELETE:
                apiResult = HttpUtils.delete(regionUrl.toString(), possibleRemote.responseType());
                break;
            default:
                logger.error("[[tag=remoteHttpAop]] unsupported HttpRequestMethod" + httpRequestType.getDescription());
                return null;
        }
        logger.info("[[tag=remoteHttpAop]] remote invoke console via Http url:{}", regionUrl);
        return apiResult.getData();
    }

    private void spliceUrl(StringBuilder regionUrl, Map<String, Object> args) {
        boolean isFirstArgs = true;
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            if (isFirstArgs) {
                regionUrl.append("?");
                isFirstArgs = false;
            } else {
                regionUrl.append("&");
            }
            String str = parseArgValue(entry.getValue());
            regionUrl.append(entry.getKey()).append("=").append(str);
        }
    }

    @VisibleForTesting
    public static String parseArgValue(Object value) {
        if (value instanceof List) {
            StringBuilder sb = new StringBuilder();
            List list = (List) value;
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i));
                if (i != list.size() - 1) {
                    sb.append(",");
                }
            }
            return sb.toString();
        }
        return String.valueOf(value);
    }

    private String getDcNameByArgs(Map<String, Object> arguments, PossibleRemote possibleRemote) {
        switch (possibleRemote.httpType()) {
            case GET:
                return getDcName(arguments);
            case POST:
                String paramValue = JsonUtils.toJson(arguments.get("requestBody"));
                Map<String, Object> paramMap = JsonUtils.fromJson(paramValue, new TypeToken<Map<String, Object>>(){}.getType());
                return getDcName(paramMap);
            default:
                logger.error("[[tag=remoteHttpAop]] unsupported HttpRequestMethod");
                return null;
        }
    }

    private String getDcName(Map<String, Object> arguments) {
        String dcName = null;
        try {
            if (arguments.containsKey("mha") || arguments.containsKey("mhaName") ||
                    arguments.containsKey("dc") || arguments.containsKey("dcName")) {
                if (arguments.containsKey("dc")) {
                    dcName = (String) arguments.get("dc");
                } else if (arguments.containsKey("dcName")) {
                    dcName = (String) arguments.get("dcName");
                } else if (arguments.containsKey("mha")) {
                    dcName = getDcName((String) arguments.get("mha"));
                } else if (arguments.containsKey("mhaName")) {
                    dcName = getDcName((String) arguments.get("mhaName"));
                }
            }
        } catch (SQLException e) {
            logger.error("[[tag=remoteHttpAop]] sql error", e);
            return null;
        }
        return dcName;
    }

    private String getDcName(String mha) throws SQLException {
        if (!mha2DcMap.containsKey(mha)) {
            String dcName = centralService.getDcName(mha);
            if (StringUtils.isEmpty(dcName)) {
                logger.warn("[[tag=remoteHttpAop]] dcName is empty, mha: {}", mha);
                return dcName;
            }
            mha2DcMap.put(mha, dcName);
        }
        return mha2DcMap.get(mha);
    }

    private String getRegionByArgs(Map<String, Object> arguments, PossibleRemote possibleRemote) {
        if (arguments.containsKey("region") &&
                consoleConfig.getRegion2dcsMapping().containsKey((String) arguments.get("region"))) {
            return (String) arguments.get("region");
        }
        String dcNameByArgs = getDcNameByArgs(arguments, possibleRemote);
        if (StringUtils.isEmpty(dcNameByArgs)) {
            logger.warn("[[tag=remoteHttpAop]] no region find by arguments");
            return null;
        }  else {
            return consoleConfig.getRegionForDc(dcNameByArgs);
        }
    }

    /**
     * 获取参数Map集合
     * @param joinPoint
     * @return
     */
    private Map<String, Object> getArgsNotExcluded(ProceedingJoinPoint joinPoint,String[] excludeArguments) throws NoSuchMethodException{
        HashSet<String> excludeArgs = Sets.newHashSet(excludeArguments);
        Map<String, Object> param = new HashMap<>();
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] paramValues = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();
        for (int i = 0; i < paramNames.length; i++) {
            if (excludeArgs.contains(paramNames[i])) {
                continue;
            }
            logger.debug("[[tag=remoteHttpAop]] method:{},argName:{},argValue:{}",method.getName(),paramNames[i],paramValues[i]);
            param.put(paramNames[i], paramValues[i]);
        }
        return param;
    }

}
    
