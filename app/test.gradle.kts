tasks.register("runLogicTest") {
    doLast {
        val classNames = listOf("IX-A", "IX-B", "VI-A", "IX")
        for (className in classNames) {
            println("Class: \$className")
        }
    }
}
