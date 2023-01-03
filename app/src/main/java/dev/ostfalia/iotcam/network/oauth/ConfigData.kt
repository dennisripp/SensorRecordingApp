package dev.ostfalia.iotcam.network.oauth

class ConfigData {
    companion object {
        const val BASE: String = "https://super-pipeline-endpoint01-uiop.auth.eu-central-1.amazoncognito.com"
        val AUTH_URL: String =
            "$BASE/oauth2/authorize";
        val ACCESS_TOKEN_URL: String =
            "$BASE/oauth2/token";
        val CLIENT_ID: String = "6mqkcucvqs8m0cgcubrbpg4l3q";
    }

    //ireland
    /*
    companion object {
        val AUTH_URL: String =
            "https://superpipeline.auth.eu-west-1.amazoncognito.com/oauth2/authorize";
        val ACCESS_TOKEN_URL: String =
            "https://superpipeline.auth.eu-west-1.amazoncognito.com/oauth2/token";
        val CLIENT_ID: String = "5q5pufa0dleqnrbbl8bft2u722";
        val CLIENT_SECRET: String = "181b3gq7e07s7skv0cr974jombad9lvg15srnnef4bc1cq9ur100";
    } */
}