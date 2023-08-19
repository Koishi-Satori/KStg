cmake_minimum_required(VERSION 3.20)
project(cpp_bootstrapper)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_BINARY_DIR}/lib)

add_library(util SHARED lib/util.cpp)
add_executable(cpp_bootstrapper main.cpp)
link_libraries(${PROJECT_SOURCE_DIR}/lib)
target_link_libraries(cpp_bootstrapper util)

