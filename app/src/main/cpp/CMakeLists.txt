# Copyright (c) 2025 Ismail Abdullah. All rights reserved.
#
# This software and all associated files are the exclusive property of Ismail Abdullah.
# Unauthorized use, copying, modification, or distribution is strictly prohibited.
#
# Contact: ismail.t.abdullah@gmail.com

cmake_minimum_required(VERSION 3.22.1)
project(aiish_native)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -DNDEBUG -fvisibility=hidden")

# ARM NEON optimizations for mobile
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a+fp+simd")
endif()

# Find required libraries
find_library(log-lib log)
find_library(android-lib android)

# Source files
set(SOURCES
    llm_bridge.cpp
)

# Create shared library
add_library(aiish_native SHARED ${SOURCES})

# Link libraries
target_link_libraries(aiish_native
    ${log-lib}
    ${android-lib}
)

# Include directories
target_include_directories(aiish_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)
