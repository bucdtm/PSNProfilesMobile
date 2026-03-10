package com.example.psnprofilesmobile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

// Android imports
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

// import java.io.BufferedReader; Deprecated imports will no longer be used with the new website parser
import java.io.InputStream;
// import java.io.InputStreamReader; Deprecated imports will no longer be used with the new website parser

// JSoup imports
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // Declaration of a WebView to interact with the webview element
    private WebView myWebView;
    private String userAgent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We set the content view to the layout we defined before in activity_main.xml
        setContentView(R.layout.activity_main);

        // Find the WebView element by its ID
        myWebView = findViewById(R.id.webView);

        // We get the WebSettings object to configure the WebView settings
        WebSettings webSettings = myWebView.getSettings();

        // Enable JavaScript
        webSettings.setJavaScriptEnabled(true);
        // We activate the DOM storage
        webSettings.setDomStorageEnabled(true);
        // If not, uses network
        webSettings.setCacheMode(webSettings.LOAD_DEFAULT);
        // Hardware Acceleration to improve performance
        myWebView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);
        // Images dont load first to improve performance !!CHECK LATER onPageFinished()
        // METHOD
        webSettings.setLoadsImagesAutomatically(false);
        // Save User-Agent for Jsoup interceptor
        userAgent = webSettings.getUserAgentString();
        // We activate cookies
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        // We accept third-party cookies
        cookieManager.setAcceptThirdPartyCookies(myWebView, true);

        // Set WebViewClient to ensure links open within the WebView, not the browser
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // We load the images after the page has finished loading
                view.getSettings().setLoadsImagesAutomatically(true);
            }

            // Completely revamped interceptor method to inject CSS before it loads,
            // and to fetch the cookies from the WebView so the connection stays logged in!
            // old CSS injection is greatly deprecated after using jsoup (API that fetches
            // data)
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view,
                    android.webkit.WebResourceRequest request) {
                String url = request.getUrl().toString();

                // We only want to intercept the main HTML documents (GET requests) to inject
                // our CSS before it loads
                if (request.isForMainFrame() && request.getMethod().equalsIgnoreCase("GET")
                        && url.contains("psnprofiles.com")) {
                    try {
                        // Fetch the cookies from the WebView so the connection stays logged in!
                        String cookies = android.webkit.CookieManager.getInstance().getCookie(url);

                        // Use JSoup to fetch the HTML content
                        org.jsoup.Connection connection = Jsoup.connect(url)
                                .userAgent(userAgent);
                        if (cookies != null) {
                            connection.header("Cookie", cookies);
                        }
                        Document document = connection.get();

                        // Add the Viewport Meta tag
                        document.head().append(
                                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">");

                        // Add our custom CSS directly to the HEAD
                        String css = getCSSFromAssets();
                        document.head().append("<style>" + css + "</style>");

                        // Return the modified HTML back to the WebView as a WebResourceResponse
                        InputStream modifiedHtml = new java.io.ByteArrayInputStream(
                                document.outerHtml().getBytes("UTF-8"));
                        return new android.webkit.WebResourceResponse("text/html", "UTF-8", modifiedHtml);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // For all other requests (images, standard css, scripts), let them load
                // normally
                return super.shouldInterceptRequest(view, request);
            }
        });

        myWebView.loadUrl("https://psnprofiles.com/guides/popular");
    }

    // New function that reassures the user not to quit the app if he tries to go
    // back or presses the back button
    @Override
    public void onBackPressed() {
        if (myWebView != null && myWebView.canGoBack()) {
            // If the WebView can go back, it will go back
            myWebView.goBack();
        } else {
            // Otherwise, it will exit the app
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (myWebView != null) {
            myWebView.destroy();
        }
        super.onDestroy();
    }

    // We fetch the CSS from the assets folder as a String so JSoup can inject it
    // into the HTML document before serving it
    private String getCSSFromAssets() {
        try (InputStream inputStream = getAssets().open("style_psn.css")) {

            // We allocate a byte array of the exact size of the file and read it all at
            // once
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);

            // We extract the content from the buffer as an immutable String
            return new String(buffer);

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
