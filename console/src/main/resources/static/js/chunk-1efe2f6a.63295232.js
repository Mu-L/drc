(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-1efe2f6a"],{"0e27":function(t,i,e){"use strict";e.r(i);var n=function(){var t=this,i=t.$createElement,e=t._self._c||i;return e("base-component",[e("Breadcrumb",{style:{margin:"15px 0 15px 185px",position:"fixed"}},[e("BreadcrumbItem",{attrs:{to:"/home"}},[t._v("首页")]),e("BreadcrumbItem",{attrs:{to:{path:"/accessV2",query:{step:3,clustername:this.initInfo.srcMha,newclustername:this.initInfo.destMha,order:this.initInfo.order}}}},[t._v("DRC配置")]),e("BreadcrumbItem",[t._v("同步表")])],1),e("Content",{staticClass:"content",style:{padding:"10px",background:"#fff",margin:"50px 0 1px 185px",zIndex:"1"}},[e("Row",[e("Col",{attrs:{span:"22"}},[e("span",{staticStyle:{"margin-top":"10px",color:"#464c5b","font-weight":"600"}},[t._v(t._s(t.initInfo.srcMha)+"("+t._s(t.initInfo.srcDc)+")==>"+t._s(t.initInfo.destMha)+"("+t._s(t.initInfo.destDc)+")")])]),e("Col",{attrs:{span:"2"}},[e("Button",{staticStyle:{"margin-top":"10px","text-align":"right"},attrs:{type:"primary",ghost:""},on:{click:t.goToTableConfigFlow}},[t._v("添加")])],1)],1),e("div",{style:{padding:"1px 1px",height:"100%"}},[[e("Table",{staticStyle:{"margin-top":"20px"},attrs:{stripe:"",columns:t.columns,data:t.tableData,border:""},scopedSlots:t._u([{key:"action",fn:function(i){var n=i.row,o=i.index;return[e("Button",{staticStyle:{"margin-right":"5px"},attrs:{type:"success",size:"small"},on:{click:function(i){return t.goToShowConfig(n,o)}}},[t._v("查看")]),e("Button",{staticStyle:{"margin-right":"5px"},attrs:{type:"primary",size:"small"},on:{click:function(i){return t.goToUpdateConfig(n,o)}}},[t._v("修改")]),e("Button",{staticStyle:{"margin-right":"10px"},attrs:{type:"error",size:"small"},on:{click:function(i){return t.goToDeleteConfig(n,o)}}},[t._v("删除")])]}}])})]],2)],1)],1)},o=[],a=(e("b0c0"),{name:"tables",data:function(){return{initInfo:{srcMha:"",srcMhaId:0,destMha:"",applierGroupId:0,srcDc:"",destDc:"",order:!0},columns:[{title:"序号",width:75,align:"center",fixed:"left",render:function(t,i){return t("span",i.index+1)}},{title:"库名",key:"namespace"},{title:"表名",key:"name"},{title:"操作",slot:"action",align:"center",width:200,fixed:"right"}],tableData:[],total:0,size:5,pageSizeOpts:[5,10,20,100]}},methods:{getAllTableVosInApplierGroup:function(){var t=this;console.log(this.initInfo.applierGroupId),this.axios.get("/api/drc/v1/dataMedia/vos?applierGroupId="+this.initInfo.applierGroupId).then((function(i){1===i.data.status?window.alert("查询相关配置表失败!"):t.tableData=i.data.data}))},goToTableConfigFlow:function(){this.$router.push({path:"/tables/configFlow",query:{srcMha:this.initInfo.srcMha,srcMhaId:this.initInfo.srcMhaId,destMha:this.initInfo.destMha,applierGroupId:this.initInfo.applierGroupId,srcDc:this.initInfo.srcDc,destDc:this.initInfo.destDc,order:this.initInfo.order,dataMediaId:0,namespace:"",name:""}})},goToShowConfig:function(t,i){},goToUpdateConfig:function(t,i){this.$router.push({path:"/tables/configFlow",query:{srcMha:this.initInfo.srcMha,srcMhaId:this.initInfo.srcMhaId,destMha:this.initInfo.destMha,applierGroupId:this.initInfo.applierGroupId,srcDc:this.initInfo.srcDc,destDc:this.initInfo.destDc,order:this.initInfo.order,dataMediaId:t.id,namespace:t.namespace,name:t.name}})},goToDeleteConfig:function(t,i){var e=this;console.log(t),this.axios.delete("/api/drc/v1/dataMedia/dataMediaConfig/"+t.id).then((function(t){1===t.data.status?window.alert("删除失败!"):(window.alert("删除成功!"),e.getAllTableVosInApplierGroup())}))}},created:function(){this.initInfo={srcMha:this.$route.query.srcMha,srcMhaId:this.$route.query.srcMhaId,destMha:this.$route.query.destMha,applierGroupId:this.$route.query.applierGroupId,srcDc:this.$route.query.srcDc,destDc:this.$route.query.destDc,order:this.$route.query.order},console.log("initInfo:"),console.log(this.initInfo),this.getAllTableVosInApplierGroup()}}),r=a,s=e("2877"),c=Object(s["a"])(r,n,o,!1,null,"1bbbe414",null);i["default"]=c.exports},b0c0:function(t,i,e){var n=e("83ab"),o=e("9bf2").f,a=Function.prototype,r=a.toString,s=/^\s*function ([^ (]*)/,c="name";!n||c in a||o(a,c,{configurable:!0,get:function(){try{return r.call(this).match(s)[1]}catch(t){return""}}})}}]);
//# sourceMappingURL=chunk-1efe2f6a.63295232.js.map