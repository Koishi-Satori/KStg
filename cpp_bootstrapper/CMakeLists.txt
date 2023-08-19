cmake_minimum_required(VERSION 3.20)
project(cpp_bootstrapper)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(LIBRARY_OUTPUT_PATH ${CMAKE_BINARY_DIR}/bin)

add_library(util SHARED bin/util.cpp)
add_executable(KStg main.cpp)
link_libraries(${CMAKE_BINARY_DIR}/bin)
target_link_libraries(KStg util)

