package io.github.stardust824

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font
import plant_app.shared.generated.resources.Merriweather
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.savedstate.read

import plant_app.shared.generated.resources.Res
import plant_app.shared.generated.resources.compose_multiplatform
import plant_app.shared.generated.resources.logo_purple
import plant_app.shared.generated.resources.plant_app_logo

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun App() {
    val Merriweather = FontFamily(
        Font(Res.font.Merriweather)
    )
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Track current route to highlight active item in drawer
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet (
                drawerContainerColor = Color(0xFFFFFFFF),
                modifier = Modifier.width(220.dp),
                drawerShape = RectangleShape,
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                ) {
                    Text("Menu", fontSize = 22.sp,
                        fontFamily = Merriweather,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    NavigationDrawerItem(
                        label = { Text("Home") },
                        shape = RectangleShape,
                        selected = currentRoute == Screen.Home.route,
                        colors = NavigationDrawerItemDefaults.colors(
                            // background of button is a slightly duller green than main buttons
                            selectedContainerColor = Color(0xFF4FBA77), // Background when selected
                            selectedIconColor = Color.White,            // Icon color when selected
                            selectedTextColor = Color.White,           // Text color when selected
                            unselectedContainerColor = Color.White, // Background when not selected
                            unselectedTextColor = Color.Black          // Text color when not selected
                        ),
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Home.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("ID a Plant") },
                        shape = RectangleShape,
                        selected = currentRoute == Screen.ID.route,
                        colors = NavigationDrawerItemDefaults.colors(
                            // background of button is a slightly duller green than main buttons
                            selectedContainerColor = Color(0xFF4FBA77), // Background when selected
                            selectedIconColor = Color.White,            // Icon color when selected
                            selectedTextColor = Color.White,           // Text color when selected
                            unselectedContainerColor = Color.White, // Background when not selected
                            unselectedTextColor = Color.Black          // Text color when not selected
                        ),
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.ID.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Browse Plants") },
                        shape = RectangleShape,
                        selected = currentRoute == Screen.PlantList.route,
                        colors = NavigationDrawerItemDefaults.colors(
                            // background of button is a slightly duller green than main buttons
                            selectedContainerColor = Color(0xFF4FBA77), // Background when selected
                            selectedIconColor = Color.White,            // Icon color when selected
                            selectedTextColor = Color.White,           // Text color when selected
                            unselectedContainerColor = Color.White, // Background when not selected
                            unselectedTextColor = Color.Black          // Text color when not selected
                        ),
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.PlantList.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("My Field Guide") },
                        shape = RectangleShape,
                        selected = currentRoute == Screen.FieldGuide.route,
                        colors = NavigationDrawerItemDefaults.colors(
                            // background of button is a slightly duller green than main buttons
                            selectedContainerColor = Color(0xFF4FBA77), // Background when selected
                            selectedIconColor = Color.White,            // Icon color when selected
                            selectedTextColor = Color.White,           // Text color when selected
                            unselectedContainerColor = Color.White, // Background when not selected
                            unselectedTextColor = Color.Black          // Text color when not selected
                        ),
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.FieldGuide.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("About") },
                        shape = RectangleShape,
                        selected = currentRoute == Screen.About.route,
                        colors = NavigationDrawerItemDefaults.colors(
                            // background of button is a slightly duller green than main buttons
                            selectedContainerColor = Color(0xFF4FBA77), // Background when selected
                            selectedIconColor = Color.White,            // Icon color when selected
                            selectedTextColor = Color.White,           // Text color when selected
                            unselectedContainerColor = Color.White, // Background when not selected
                            unselectedTextColor = Color.Black          // Text color when not selected
                        ),
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.About.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) {
        // Main UI: page content with a floating menu button in the corner
        Box(modifier = Modifier.fillMaxSize()) {
            // Host that handles page switching inside the drawer wrapper
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) {
                    Home(
                        onIdClick = {
                            navController.navigate(Screen.ID.route) {
                                launchSingleTop = true
                            }
                        },
                        onBrowseClick = {
                            navController.navigate(Screen.PlantList.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                // Opens a plant's page, e.g. from Browse Plants or Best matches
                val openPlant: (String) -> Unit = { plantId ->
                    navController.navigate(Screen.PlantDetail.routeFor(plantId))
                }
                composable(Screen.ID.route) { ID(onPlantClick = openPlant) }
                composable(Screen.PlantList.route) { PlantList(onPlantClick = openPlant) }
                composable(Screen.FieldGuide.route) { FieldGuide() }
                composable(Screen.About.route) { About() }
                composable(Screen.PlantDetail.route) { entry ->
                    // Read the {id} part of "plant/{id}"
                    val plantId = entry.arguments?.read { getStringOrNull(Screen.PlantDetail.ID_ARG) }
                    PlantDetailScreen(plantId)
                }
            }

            // Floating button in the top-left corner:
            // on a plant's page it's a back arrow, everywhere else it opens the menu.
            val onPlantPage = currentRoute == Screen.PlantDetail.route
            IconButton(
                onClick = {
                    if (onPlantPage) navController.popBackStack()
                    else scope.launch { drawerState.open() }
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .safeContentPadding()
                    .padding(8.dp)
            ) {
                Text(if (onPlantPage) "←" else "☰", fontSize = 24.sp)
            }
        }
    }
}

// DO NOT TOUCH OR CHANGE THE HOME FUNCTION
@Composable
@Preview
fun Home(
    onIdClick: () -> Unit = {},
    onBrowseClick: () -> Unit = {},
) {
    val Merriweather = FontFamily(
        Font(Res.font.Merriweather)
    )
    MaterialTheme {



        Column(
            modifier = Modifier
                .background(Color(0xFFFFFFFF))
                .safeContentPadding()
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Plants of Western Washington",
                fontSize = 24.sp,
                fontFamily = Merriweather,
                textAlign = TextAlign.Center,
                // keep clear of the floating menu button in the top-left corner
                modifier = Modifier.padding(horizontal = 48.dp)
                )
            Spacer(modifier = Modifier.height(20.dp))
            Image(
                painter = painterResource(Res.drawable.logo_purple),
                contentDescription = "Artistic Evergreen Huckleberry",    // Accessibility text for screen readers
                modifier = Modifier.size(300.dp)
            )
            Spacer(modifier  = Modifier.weight(1f))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp), // Distance between the two button
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onIdClick,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3DBB6C),
                        contentColor = Color.White
                    )
                ) {
                    Text("ID a Plant")
                }
                Button(
                    onClick = onBrowseClick,
                    modifier = Modifier.padding(bottom = 30.dp),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3DBB6C),
                        contentColor = Color.White
                    )


                ) {
                    Text("Browse Plants")
                }
            }
        }
    }
}
@Composable
fun ID(onPlantClick: (plantId: String) -> Unit = {}) {
    // The ID screen lives in IdScreen.kt
    IdScreen(onPlantClick = onPlantClick)
}
@Composable
fun PlantList(onPlantClick: (plantId: String) -> Unit = {}) {
    // The Browse Plants screen lives in BrowseScreen.kt
    BrowseScreen(onPlantClick = onPlantClick)
}
@Composable
@Preview
fun FieldGuide() {
    val Merriweather = FontFamily(
        Font(Res.font.Merriweather)
    )
    MaterialTheme {
        // Placeholder page until the field guide is built out
        Column(
            modifier = Modifier
                .background(Color(0xFFFFFFFF))
                .safeContentPadding()
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("My Field Guide",
                fontSize = 24.sp,
                fontFamily = Merriweather,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text("Coming soon!", textAlign = TextAlign.Center)
        }
    }
}
@Composable
@Preview
fun About() {
    val Merriweather = FontFamily(
        Font(Res.font.Merriweather)
    )
    MaterialTheme {
        // Placeholder page until the about page is built out
        Column(
            modifier = Modifier
                .background(Color(0xFFFFFFFF))
                .safeContentPadding()
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("About",
                fontSize = 24.sp,
                fontFamily = Merriweather,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text("Coming soon!", textAlign = TextAlign.Center)
        }
    }
}