cordova.define('cordova/plugin_list', function (require, exports, module) {
    module.exports = [
        {
            "file": "plugins/cordova-plugin-whitelist/whitelist.js",
            "id": "cordova-plugin-whitelist.whitelist",
            "pluginId": "cordova-plugin-whitelist",
            "runs": true
        },
        {
            "file": "plugins/com.betasoft.cordova.plugin.intent/www/android/IntentPlugin.js",
            "id": "com.betasoft.cordova.plugin.intent.IntentPlugin",
            "pluginId": "com.betasoft.cordova.plugin.intent",
            "clobbers": [
                "IntentPlugin"
            ]
        }
    ];
    module.exports.metadata =
    // TOP OF METADATA
    {
        "cordova-plugin-whitelist": "1.3.5",
        "com.betasoft.cordova.plugin.intent": "1.0.0"
    }
    // BOTTOM OF METADATA
});