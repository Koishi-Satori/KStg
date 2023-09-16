cmake_minimum_required(VERSION 3.20)
project(cpp_bootstrapper)

set(CMAKE_C_COMPILER ${MSVC_C_COMPILER})
set(CMAKE_CXX_COMPILER ${MSVC_CXX_COMPILER})

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(LIBRARY_OUTPUT_PATH ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_VERBOSE_MAKEFILE ON)

add_library(util SHARED util.cpp "util.cpp" "util.h")
add_executable(KStg main.cpp "util.cpp" "util.h")
target_link_libraries(KStg util)

