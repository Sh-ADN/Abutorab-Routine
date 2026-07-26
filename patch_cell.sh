sed -i 's/, onLongClick: (() -> Unit)? = null//g' app/src/main/java/com/abutorab/routine/MainActivity.kt
sed -i '/if (onLongClick != null) Modifier.pointerInput/,/else Modifier/d' app/src/main/java/com/abutorab/routine/MainActivity.kt
sed -i '/.then(/d' app/src/main/java/com/abutorab/routine/MainActivity.kt
sed -i '/) \/\/.then/d' app/src/main/java/com/abutorab/routine/MainActivity.kt
