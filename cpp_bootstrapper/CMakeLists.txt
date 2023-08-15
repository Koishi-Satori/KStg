cmake_minimum_required(VERSION 3.20)
project(cpp_bootstrapper)

set(CMAKE_CXX_STANDARD 14)

add_library(util SHARED util.cpp)
add_executable(cpp_bootstrapper main.cpp)
link_libraries(${PROJECT_SOURCE_DIR}/)
target_link_libraries(cpp_bootstrapper util)

