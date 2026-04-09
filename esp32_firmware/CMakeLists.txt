cmake_minimum_required(VERSION 3.5)

project(agropulse_esp32)

# Componente principal
add_executable(${CMAKE_PROJECT_NAME} main/app_main.c)

# Agregar componentes ESP-IDF
idf_build_get_properties(target_properties COMPONENTS ${COMPONENTS})
set(EXTRA_COMPONENT_DIRS "")
