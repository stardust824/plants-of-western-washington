package io.github.stardust824

sealed class Screen(val route: String) {
    data object FieldGuide : Screen("field_guide")
    data object Home : Screen("home")
    // used for browsing plants but all possible plants
    data object PlantList: Screen("plants_list")
    data object ID: Screen("id")
    data object About: Screen("about")
    // One plant's page; {id} is the plant's id, e.g. "plant/red_huckleberry"
    data object PlantDetail : Screen("plant/{id}") {
        const val ID_ARG = "id"
        fun routeFor(plantId: String) = "plant/$plantId"
    }
}