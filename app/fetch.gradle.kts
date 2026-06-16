tasks.register("fetchData") {
    doLast {
        val url = java.net.URL("https://script.google.com/macros/s/AKfycbzGUBlzXFJ_OS8cDTKOvEl4-7B1TMFYvF-n_RySMP61SnUOSTcmnWll5L6-fvNrabdmKw/exec?action=getFullDatabase")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        
        // This handles the redirect from Google Apps Script
        var redirectUrl = url
        var responseCode = connection.responseCode
        var finalConnection = connection
        while (responseCode == java.net.HttpURLConnection.HTTP_MOVED_TEMP || 
               responseCode == java.net.HttpURLConnection.HTTP_MOVED_PERM || 
               responseCode == java.net.HttpURLConnection.HTTP_SEE_OTHER) {
            val newUrl = finalConnection.getHeaderField("Location")
            redirectUrl = java.net.URL(newUrl)
            finalConnection = redirectUrl.openConnection() as java.net.HttpURLConnection
            responseCode = finalConnection.responseCode
        }
        
        val content = finalConnection.inputStream.bufferedReader().use { it.readText() }
        val lines = content.lines()
        for (line in lines) {
            if (line.contains("M.U.") || line.contains("F&B") || line.contains("F&B IX")) {
                println(line)
            }
        }
        
        // Let's print out the specific masterGrid section containing M.U.
        // Assuming it's valid JSON
    }
}
