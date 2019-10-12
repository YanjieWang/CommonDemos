#这两个需要手动配置********
NDK=/mnt/c/android/android-ndk-r20
API=29
#这两个需要手动配置********

Build_OS=linux
TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/${Build_OS}-x86_64


#填入目录 x264 ffmpeg zlib openssl nginx
NEED_BUILD_PROJECTS="nginx"

#填入需要编译的架构 armv8-a armv7-a x86 x86_64
NEED_BUILD_ARCH="armv8-a"

function build_nginx
{
    echo "Compiling nginx for $CPU"
    sed -i "s/        exit 1/        ngx_max_value=2147483647\r        ngx_max_len=\'\(sizeof\(\"-2147483648\"\) - 1\)\'/g" auto/types/sizeof
    make clean
    export PATH=$PATH:${PREFIX}/lib
    ./auto/configure \
        --prefix=$PREFIX \
        --conf-path=/sdcard/nginx/conf/nginx.conf \
        --error-log-path=/sdcard/nginx/error.log \
        --http-log-path=/sdcard/nginx/access.log \
        --with-http_stub_status_module \
        --with-http_gzip_static_module \
        --http-client-body-temp-path=/sdcard/nginx/client/ \
        --http-proxy-temp-path=/sdcard/nginx/proxy/ \
        --http-fastcgi-temp-path=/sdcard/nginx/fcgi/ \
        --http-uwsgi-temp-path=/sdcard/nginx/uwsgi \
        --http-scgi-temp-path=/sdcard/nginx/scgi \
        --without-http_rewrite_module

    make -j16
#    make install -j16

    echo "The Compiling of nginx for $CPU is completed"
}

function build_zlib
{
    echo "Compiling zlib for $CPU"

    ./configure \
        --prefix=$PREFIX \
        --static \
        --sysconfdir=$SYSROOT \
        --shared

    #去除so后面的版本号，android不支持形如libdemo.so.3.4形式命名的so库
    sed -i "s/SHAREDLIBV=libz.so.*/SHAREDLIBV=libz.so/g" Makefile
    sed -i "s/SHAREDLIBM=libz.so.*/SHAREDLIBM=libz.so.1/g" Makefile
    sed -i "s/SHAREDLIB=libz.so.*/SHAREDLIB=libz.so.2/g" Makefile
    sed -i "s/ln -s/echo/g" Makefile
    make clean
    make -j16
    make install -j16

    echo "The Compiling of zlib for $CPU is completed"
}


function build_openssl
{
    echo "Compiling openssl for $CPU"
    export RANLIB=${CROSS_PREFIX}ranlib
    #去除-mandroid clang不支持，否则编译报错
    sed -i "s/-mandroid//g" Configurations/10-main.conf
    if [ ${ARCH} == "arm64" ];then
        ARCH=android64-aarch64
    elif [ ${ARCH} == "arm" ];then
        ARCH=android-armeabi
    elif [ ${ARCH} == "x86" ];then
        ARCH=android-x86
    else
        echo openssl 不支持此CPU架构:${ARCH}
        return
    fi
    ./configure $ARCH \
        --prefix=$PREFIX \
        --openssldir=$PREFIX \
        -Wl,-rpath,$PREFIX/lib \
        zlib \
        --sysroot=$SYSROOT
#        -static \

    #去除so后面的版本号，android不支持形如libdemo.so.3.4形式命名的so库
    sed -i "s/\.\$(SHLIB_MAJOR).\$(SHLIB_MINOR)//g" Makefile

    #去除生成文档，个人觉得生成文档太浪费编译时间了
    sed -i "s/install_sw install_ssldirs install_docs/install_sw install_ssldirs/g" Makefile
    make clean
    make -j16
    #多线程执行一遍无法生成库文件，需要执行两遍
    make install -j16
    make install -j16

    echo "The Compiling of openssl for $CPU is completed"
}


function build_x265
{
cd x265/build/arm-linux/
echo "Compiling x625 for $CPU"
#sed -i "s/armv6l/${CC//\//\\\/}/g" crosscompile.cmake
#sed -i "s/arm-linux-gnueabi-g++/${CXX//\//\\\/}/g" crosscompile.cmake
#sed -i "s/\/usr\/arm-linux-gnueabi/${SYSROOT//\//\\\/}/g" crosscompile.cmake
echo "set(CROSS_COMPILE_ARM 1)">crosscompile.cmake
echo "set(CMAKE_SYSTEM_NAME Linux)">>crosscompile.cmake
echo "set(CMAKE_SYSTEM_PROCESSOR ${CPU})">>crosscompile.cmake
echo "set(CMAKE_C_COMPILER ${CC})">>crosscompile.cmake
echo "set(CMAKE_CXX_COMPILER ${CXX})">>crosscompile.cmake
echo "SET(EXECUTABLE_OUTPUT_PATH ${PREFIX}/bin)">>crosscompile.cmake
echo "SET(LIBRARY_OUTPUT_PATH ${PREFIX}/lib)">>crosscompile.cmake
echo "SET(BIN_INSTALL_DIR ${PREFIX}/bin)">>crosscompile.cmake
echo "SET(CMAKE_AR ${PREFIX}/${CROSS_PREFIX}ar)">>crosscompile.cmake
echo "SET(CMAKE_STRIP ${PREFIX}/${CROSS_PREFIX}strip)">>crosscompile.cmake
echo "SET(ENABLE_PIC on)">>crosscompile.cmake

cmake -DCMAKE_TOOLCHAIN_FILE=crosscompile.cmake -G "Unix Makefiles" ../../source && ccmake -DCMAKE_TOOLCHAIN_FILE=crosscompile.cmake -G "Unix Makefiles" ../../source
make
echo "The Compiling of x624 for $CPU is completed"
cd ../../../
}

function build_x264
{
    echo "Compiling x264 for $CPU"
    ./configure --prefix=$PREFIX \
        --enable-static \
        --enable-shared \
        --enable-pic \
        --enable-cli \
        --disable-opencl \
        --cross-prefix=$CROSS_PREFIX \
        --extra-ldexeflags="-static" \
        --sysroot=$SYSROOT \
        --host=$HOST
    sed -i "s/SONAME=libx264.so.*/SONAME=libx264.so/g" config.mak
    make clean
    make -j16
    make install -j16
    echo "The Compiling of x264 for $CPU is completed"
}
function build_ffmpeg
{
    echo "Compiling FFmpeg for $CPU"
    sed -i "s/SLIBNAME_WITH_MAJOR='\$(SLIBNAME).\$(LIBMAJOR)'/SLIBNAME_WITH_MAJOR='\$(SLIBPREF)\$(FULLNAME)-\$(LIBMAJOR)\$(SLIBSUF)'/g" configure
    sed -i "s/SLIB_INSTALL_NAME='\$(SLIBNAME_WITH_VERSION)'/SLIB_INSTALL_NAME='\$(SLIBNAME_WITH_MAJOR)'/g" configure
    sed -i "s/SLIB_INSTALL_LINKS='\$(SLIBNAME_WITH_MAJOR) \$(SLIBNAME)'/SLIB_INSTALL_LINKS='\$(SLIBNAME)'/g" configure
    ./configure \
        --prefix=$PREFIX \
        --disable-x86asm \
        --enable-neon \
        --enable-pic \
        --enable-hwaccels \
        --enable-gpl \
        --enable-nonfree \
        --enable-version3 \
        --enable-postproc \
        --enable-shared \
        --enable-static \
        --enable-libx264 \
        --extra-cflags="-I${PREFIX}/include -Os $OPTIMIZE_CFLAGS" \
        --extra-ldflags="-L${PREFIX}/lib" \
        --extra-ldexeflags="-static" \
        --enable-jni \
        --enable-mediacodec \
        --enable-doc \
        --enable-ffmpeg \
        --enable-ffplay \
        --enable-mediacodec \
        --enable-decoder=h264_mediacodec \
        --enable-decoder=hevc_mediacodec \
        --enable-decoder=mpeg4_mediacodec \
        --enable-decoder=vp8_mediacodec \
        --enable-decoder=vp9_mediacodec \
        --enable-ffprobe \
        --enable-avdevice \
        --disable-doc \
        --enable-symver \
        --enable-gpl \
        --cross-prefix=$CROSS_PREFIX \
        --target-os=android \
        --arch=$ARCH \
        --cpu=$CPU \
        --cc=$CC \
        --cxx=$CXX \
        --enable-cross-compile \
        --sysroot=$SYSROOT \
        $ADDITIONAL_CONFIGURE_FLAG
    make clean
    make -j16
    make install -j16
    echo "The Compiling of FFmpeg for $CPU is completed"
}

function init_common {
    if [ "$HOST" = "arm-linux-androideabi" ];then
        export CC=$TOOLCHAIN/bin/armv7a-linux-androideabi$API-clang
        export CXX=$TOOLCHAIN/bin/armv7a-linux-androideabi$API-clang++
        export CPP=$TOOLCHAIN/bin/armv7a-linux-androideabi$API-clang++
    else
        export CC=$TOOLCHAIN/bin/${HOST}$API-clang
        export CXX=$TOOLCHAIN/bin/${HOST}$API-clang++
        export CPP=$TOOLCHAIN/bin/${HOST}$API-clang++
    fi
    SYSROOT=${TOOLCHAIN}/sysroot
    CROSS_PREFIX=$TOOLCHAIN/bin/${HOST}-
    PREFIX=$(pwd)/android/$CPU
    MYDIR=$(pwd)
}

function init_armv8-a {
    echo "init_armv8-a called *************"
    #armv8-a
    ARCH=arm64
    CPU=armv8-a
    HOST=aarch64-linux-android
    OPTIMIZE_CFLAGS="-march=$CPU -mfloat-abi=softfp -mfpu=neon"
    init_common
}

function init_armv7-a {
    echo "init_armv7-a called *************"
    #armv7-a
    ARCH=arm
    CPU=armv7-a
    HOST=arm-linux-androideabi
    OPTIMIZE_CFLAGS="-mfloat-abi=softfp -mfpu=vfp -marm -march=$CPU "
    init_common
    export CC=$TOOLCHAIN/bin/armv7a-linux-androideabi$API-clang
    export CXX=$TOOLCHAIN/bin/armv7a-linux-androideabi$API-clang++
}

function init_x86 {
    echo "init_x86 called *************"
    #x86
    ARCH=x86
    CPU=x86
    HOST=i686-linux-android
    OPTIMIZE_CFLAGS="-march=i686 -mtune=intel -mssse3 -mfpmath=sse -m32"
    init_common
}

function init_x86_64 {
    echo "init_x86_64 called *************"
    #x86_64
    ARCH=x86_64
    CPU=x86-64
    HOST=x86_64-linux-android
    OPTIMIZE_CFLAGS="-march=$CPU -msse4.2 -mpopcnt -m64 -mtune=intel"
    init_common
}

if [ -e "$NDK" ]; then
    echo "NDK is configed to:$NDK   *************"
else
    echo "NDK is configed to:$NDK   *************"
    echo "But the path not exist,please chang the NDK value"
exit
fi

for project in ${NEED_BUILD_PROJECTS};do
    if [ -e "$project" ]; then
        for prjarch in ${NEED_BUILD_ARCH};do
            echo "init ${prjarch} for ${project} start *************"
            `echo init_$prjarch`
            echo "init ${prjarch} for ${project} end *************"
            echo "build ${project} for ${prjarch} start *************"
            cd $project
                `echo build_$project`
            cd ..
            echo "build ${project} for ${prjarch} end *************"
        done
    else
        echo "error:project folder not exist:${project}*************"
    fi
done