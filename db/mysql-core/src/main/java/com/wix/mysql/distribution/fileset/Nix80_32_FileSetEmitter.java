package com.wix.mysql.distribution.fileset;

import com.wix.mysql.distribution.Version;
import de.flapdoodle.embed.process.config.store.FileSet;
import de.flapdoodle.embed.process.distribution.Platform;

import java.util.Objects;

import static de.flapdoodle.embed.process.config.store.FileType.Executable;
import static de.flapdoodle.embed.process.config.store.FileType.Library;
import static de.flapdoodle.embed.process.distribution.Platform.OS_X;

public class Nix80_32_FileSetEmitter extends Nix implements FileSetEmitter {
    @Override
    public boolean matches(Platform platform, Version version) {
        return platform.isUnixLike() && (Platform.detect() != OS_X)
                && Objects.equals(version.getMajorVersion(), "8.0")
                && version.getMinorVersion() == Version.v8_0_32.getMinorVersion();
    }

    @Override
    public FileSet emit() {
        return FileSet.builder()
                .addEntry(Executable, "bin/mysqld")
                .addEntry(Library, "bin/mysql")
                .addEntry(Library, "bin/mysqladmin")
                .addEntry(Library, "bin/my_print_defaults")
                .addEntry(Library, "share/english/errmsg.sys")
                .addEntry(Library, "lib/private/libcrypto.so.1.1")
                .addEntry(Library, "lib/private/libssl.so.1.1")
                .addEntry(Library, "lib/private/libprotobuf-lite.so.3.19.4")
                .build();
    }
}
