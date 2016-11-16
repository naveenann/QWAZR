<#assign swaggerUiPath = request.getAttribute('swaggerUiPath')!'/api'/>
<#assign swaggerJsonPath = request.getAttribute('swaggerJsonPath')!'/api/swagger.json'/>

<link href='${swaggerUiPath}/css/typography.css' media='screen' rel='stylesheet' type='text/css'/>
<link href='${swaggerUiPath}/css/reset.css' media='screen' rel='stylesheet' type='text/css'/>
<link href='${swaggerUiPath}/css/screen.css' media='screen' rel='stylesheet' type='text/css'/>
<link href='${swaggerUiPath}/css/reset.css' media='print' rel='stylesheet' type='text/css'/>
<link href='${swaggerUiPath}/css/print.css' media='print' rel='stylesheet' type='text/css'/>

<script src='${swaggerUiPath}/lib/object-assign-pollyfill.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/lib/jquery-1.8.0.min.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/lib/jquery.slideto.min.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/lib/jquery.wiggle.min.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/lib/jquery.ba-bbq.min.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/lib/handlebars-2.0.0.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/lib/js-yaml.min.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/lib/lodash.min.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/lib/backbone-min.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/swagger-ui.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/lib/highlight.9.1.0.pack.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/lib/highlight.9.1.0.pack_extended.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/lib/jsoneditor.min.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/lib/marked.js' type='text/javascript'></script>
<script src='${swaggerUiPath}/lib/swagger-oauth.js' type='text/javascript'></script>

<!-- Some basic translations -->
<!-- <script src='lang/translator.js' type='text/javascript'></script> -->
<!-- <script src='lang/ru.js' type='text/javascript'></script> -->
<!-- <script src='lang/en.js' type='text/javascript'></script> -->

<script type="text/javascript">
    $(function () {
        var url = window.location.search.match(/url=([^&]+)/);
        if (url && url.length > 1) {
            url = decodeURIComponent(url[1]);
        } else {
            url = "${swaggerJsonPath}";
        }

        hljs.configure({
            highlightSizeThreshold: 5000
        });

        // Pre load translate...
        if (window.SwaggerTranslator) {
            window.SwaggerTranslator.translate();
        }
        window.swaggerUi = new SwaggerUi({
            url: url,
            dom_id: "swagger-ui-container",
            supportedSubmitMethods: ['get', 'post', 'put', 'delete', 'patch'],
            onComplete: function (swaggerApi, swaggerUi) {
                if (typeof initOAuth == "function") {
                    initOAuth({
                        clientId: "your-client-id",
                        clientSecret: "your-client-secret-if-required",
                        realm: "your-realms",
                        appName: "your-app-name",
                        scopeSeparator: ",",
                        additionalQueryStringParams: {}
                    });
                }

                if (window.SwaggerTranslator) {
                    window.SwaggerTranslator.translate();
                }
            },
            onFailure: function (data) {
                log("Unable to Load SwaggerUI");
            },
            docExpansion: "none",
            jsonEditor: false,
            defaultModelRendering: 'schema',
            showRequestHeaders: false,
            validatorUrl: false
        });

        window.swaggerUi.load();

        function log() {
            if ('console' in window) {
                console.log.apply(console, arguments);
            }
        }
    });
</script>