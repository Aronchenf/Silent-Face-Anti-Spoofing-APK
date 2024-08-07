cmake_minimum_required(VERSION 3.18.1)

project("engine")
# 设置C++标准
set(CMAKE_CXX_STANDARD 11)

set(CMAKE_BUILD_TYPE "release")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../../../distribution/${ANDROID_ABI})

set(ncnn_DIR ${CMAKE_SOURCE_DIR}/lib/${ANDROID_ABI}/cmake/ncnn)
find_package(ncnn REQUIRED)
include_directories(${CMAKE_SOURCE_DIR}/include)

#add_library(ncnn STATIC IMPORTED)
#set_target_properties(ncnn PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libncnn.a)

add_library(opencv_core STATIC IMPORTED)
set_target_properties(opencv_core PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_core.so)

add_library(opencv_imgproc STATIC IMPORTED)
set_target_properties(opencv_imgproc PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_imgproc.so)

add_library(opencv_codecs STATIC IMPORTED)
set_target_properties(opencv_codecs PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_imgcodecs.so)


file(GLOB jni_srcs
        "*.cpp"
        "detection/*.cpp"
        "live/*.cpp"
    )

add_library(engine SHARED ${jni_srcs})

target_link_libraries(engine log jnigraphics ncnn opencv_core opencv_imgproc opencv_codecs android)

# support 16KB
target_link_options(engine PRIVATE "-Wl,-z,max-page-size=16384")