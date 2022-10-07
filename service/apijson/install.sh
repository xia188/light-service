install_file(){
    groupId="$2" && artifactId="$3" && version="$4" && url="$1${groupId//.//}/${artifactId}/${version}/${artifactId}-${version}"
    echo "install $url.jar to $groupId:$artifactId:jar:$version"
    out="target/${artifactId}-${version}"
    if [ ! -e "$out.jar" ]; then
        echo "download jar ."
        curl -s "$url.jar" -o "$out.jar"
        echo "download pom .."
        curl -s "$url.pom" -o "$out.pom"
        echo "install-file ..."
        mvn install:install-file -DgroupId=$groupId -DartifactId=$artifactId -Dversion=$version -Dpackaging=jar -Dfile="$out.jar" -DpomFile="$out.pom"
    fi
}
[ ! -e target ] && mkdir target
repos=https://jitpack.io/
# 打开以下注释可以下载依赖jar并install
# install_file "$repos" "com.github.APIJSON" "apijson-framework" "5.2.0"
# install_file "$repos" "com.github.tencent" "APIJSON" "5.2.0"
# install_file "$repos" "com.github.APIJSON" "apijson-column" "1.2.5"
# install_file "$repos" "com.github.TommyLemon" "unitauto-java" "2.7.2"
# install_file "$repos" "com.github.TommyLemon" "unitauto-jar" "2.7.2"
# install_file "$repos" "apijson.router" "apijson-router" "1.0.6"

install_libs(){
    groupId="$2" && artifactId="$3" && version="$4" && url="$1${groupId//.//}/${artifactId}/${version}/${artifactId}-${version}"
    out="libs/${artifactId}-${version}"
    echo "install $out.jar to $groupId:$artifactId:jar:$version"
    if [ -e "$out.jar" ]; then
        echo "install-libs ..."
        mvn install:install-file -DgroupId=$groupId -DartifactId=$artifactId -Dversion=$version -Dpackaging=jar -Dfile="$out.jar" -DpomFile="$out.pom"
    fi
}
# https://gitee.com/APIJSON/APIJSON-Demo/tree/master/APIJSON-Java-Server/APIJSONBoot/libs
# 打开以下注释可以直接install仓库里的依赖jar，也可以将install.sh复制到APIJSONBoot目录并执行
# git clone https://gitee.com/APIJSON/APIJSON-Demo
# cd APIJSON-Demo/APIJSON-Java-Server/APIJSONBoot
# install_libs "$repos" "com.github.APIJSON" "apijson-framework" "5.2.0"
# install_libs "$repos" "com.github.tencent" "APIJSON" "5.2.0"
# install_libs "$repos" "com.github.APIJSON" "apijson-column" "1.2.5"
# install_libs "$repos" "com.github.TommyLemon" "unitauto-java" "2.7.2"
# install_libs "$repos" "com.github.TommyLemon" "unitauto-jar" "2.7.2"
# install_libs "$repos" "apijson.router" "apijson-router" "1.0.6"

# APIJSONBoot需要修改pom.xml注释掉spring-boot-maven-plugin然后再instal
# mvn install

# 执行上面之后就可以依赖apijson-boot了，可以复用DemoFunctionParser等demo类
# <dependency>
#     <groupId>apijson.boot</groupId>
#     <artifactId>apijson-boot</artifactId>
#     <version>5.2.0</version>
#     <exclusions>
#         <exclusion>
#             <groupId>org.springframework.boot</groupId>
#             <artifactId>spring-boot-starter-web</artifactId>
#         </exclusion>
#     </exclusions>
# </dependency>
# install_libs "$repos" "apijson.boot" "apijson-boot" "5.2.0"
