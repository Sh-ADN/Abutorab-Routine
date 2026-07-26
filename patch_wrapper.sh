# Remove the old popupInfo and AlertDialog from RoutineTable
sed -i '/var popupInfo by remember { mutableStateOf<PopupInfo?>(null) }/,/LaunchedEffect(Unit) {/c\    LaunchedEffect(Unit) {' app/src/main/java/com/abutorab/routine/MainActivity.kt

# Remove the onLongClick parameter passed to Cell in RoutineTable
sed -i 's/onLongClick = onLongClick//g' app/src/main/java/com/abutorab/routine/MainActivity.kt
sed -i 's/,  )/)/g' app/src/main/java/com/abutorab/routine/MainActivity.kt
sed -i 's/, )/)/g' app/src/main/java/com/abutorab/routine/MainActivity.kt
