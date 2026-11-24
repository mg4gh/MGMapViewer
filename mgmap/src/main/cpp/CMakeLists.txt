cmake_minimum_required(VERSION 3.22.1)
project("mgmap-native")

add_library(
        mgmap-native
        SHARED
        native-lib.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        mgmap-native
        ${log-lib}
)
