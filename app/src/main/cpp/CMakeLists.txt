cmake_minimum_required(VERSION 3.22.1)

project(rmh_tgk LANGUAGES CXX)

add_library(rmh_tgk SHARED
        tgk_rapid_native.cpp)

target_compile_features(rmh_tgk PRIVATE cxx_std_17)
target_compile_options(rmh_tgk PRIVATE
        -fvisibility=hidden
        -fno-exceptions
        -fno-rtti
        -Wall
        -Wextra
        -Werror=return-type)

target_link_libraries(rmh_tgk
        dl
        log)
