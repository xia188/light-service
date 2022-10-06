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
# https://gitee.com/APIJSON/APIJSON-Demo/tree/master/APIJSON-Java-Server/APIJSONBoot/libs
# install_file "$repos" "com.github.APIJSON" "apijson-framework" "5.2.0"
# install_file "$repos" "com.github.tencent" "APIJSON" "5.2.0"
# install_file "$repos" "com.github.APIJSON" "apijson-column" "1.2.5"
# install_file "$repos" "com.github.TommyLemon" "unitauto-java" "2.7.2"
# install_file "$repos" "com.github.TommyLemon" "unitauto-jar" "2.7.2"
# install_file "$repos" "apijson.router" "apijson-router" "1.0.6"
install_file "$repos" "apijson.boot" "apijson-boot" "5.2.0"
