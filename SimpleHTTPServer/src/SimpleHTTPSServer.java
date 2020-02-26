/**
 * @author Gayan Samuditha
 * @created 26/02/2020 - 2:32 AM
 */
public class SimpleHTTPSServer {

    private static HashMap<String, String> userList = new HashMap<>();

    //Test class
    public static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "Welcome to the chat group server !";
            HttpsExchange httpsExchange = (HttpsExchange) t;
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = t.getResponseBody();


            //write response
            os.write(response.getBytes());
            os.close();
        }
    }


    public static class registerUser implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            // parse request
            Map<String, Object> parameters = new HashMap<String, Object>();
            URI requestedUri = he.getRequestURI();
            String query = requestedUri.getRawQuery();


            String clientName = query.substring(query.indexOf("=") + 1, query.length());
            System.out.println(clientName);

            String response = "okk you are registered !";

            //Check User is in if not add
            if (userList.keySet().stream().anyMatch(clientName::equals)) {
                System.out.println("user Already Present !");
                response = "user Already Present !";
            } else {
                userList.put(clientName,"");
            }

            System.out.println(userList);
            // send response
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.toString().getBytes());

            os.close();
        }
    }


    public static class listUsers implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            // parse request
            Map<String, Object> parameters = new HashMap<String, Object>();
            URI requestedUri = he.getRequestURI();
            String query = requestedUri.getRawQuery();

            String clientName = query.substring(query.indexOf("=") + 1, query.length());
            System.out.println(clientName);

            // userList.keySet().toString().replaceAll(clientName, clientName + "(You)")
            String response = "";

            for (String name : userList.keySet()) {
                if (clientName.equals(name)) {
                    response += clientName + "(You)\n";
                } else {
                    response += name + "\n";
                }
            }

            System.out.println(userList + "Sended");
            // send response
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.toString().getBytes());

            os.close();
        }
    }

    public static class getMyMessage implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            new Thread(()->{
                try {
                    // parse request
                    Map<String, Object> parameters = new HashMap<String, Object>();
                    URI requestedUri = he.getRequestURI();
                    String query = requestedUri.getRawQuery();

                    String clientName = query.substring(query.indexOf("=") + 1, query.length());
                    System.out.println(clientName);

                    // userList.keySet().toString().replaceAll(clientName, clientName + "(You)")
                    String response = "no";

                    while (true){
                        if ( !userList.get(clientName).equals("")) {
                            response = userList.get(clientName);
                            userList.put(clientName,"");
                            break;
                        }
                    }


                    System.out.println(userList + "Sended");

                    // send response
                    he.sendResponseHeaders(200, response.length());
                    OutputStream os = he.getResponseBody();
                    os.write(response.toString().getBytes());

                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public static class sendMessages implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            // parse request
            Map<String, Object> parameters = new HashMap<String, Object>();
            URI requestedUri = he.getRequestURI();
            String query = requestedUri.getRawQuery();

            System.out.println(parameters);
            try {
                Matcher matcher2 = Pattern.compile("message=(?<msg>\\w*)&receiver=(?<rec>\\w*)&sender=(?<sen>\\w*)").matcher(query);
                if (matcher2.find()) {
                    System.out.println("message: " + matcher2.group("msg"));
                    System.out.println("receiver name: " + matcher2.group("rec"));
                    System.out.println("sender name: " + matcher2.group("sen"));

                    String message = matcher2.group("msg");
                    String receiver = matcher2.group("rec");
                    String sender = matcher2.group("sen");


                    userList.keySet().forEach(name -> {
                        if (receiver.equals(name)) {
                            //  userList.get(name).add(0, new ArrayList<String>());
                            userList.put(name, sender + "->" + message);
                        }
                    });

                    System.out.println(userList);

                    String response = "messeges sended to " + receiver;


                    // send response
                    he.sendResponseHeaders(200, response.length());
                    OutputStream os = he.getResponseBody();
                    os.write(response.toString().getBytes());

                    os.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    public static void main(String[] args) throws Exception {

        try {
            // setup the socket address
            InetSocketAddress address = new InetSocketAddress(8000);

            // initialise the HTTPS server
            HttpsServer httpsServer = HttpsServer.create(address, 0);
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // initialise the keystore
            char[] password = "123456".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream("src/myKeyStore.jks");
            ks.load(fis, password);

            // setup the key manager factory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, password);

            // setup the trust manager factory
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            // setup the HTTPS context and parameters
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);


//            System.setProperty("javax.net.ssl.trustStore", "src/myKeyStore.jks");
//            System.setProperty("javax.net.ssl.trustStorePassword", "123456");
//            System.setProperty("javax.net.debug", "ssl:record");
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    try {
                        // initialise the SSL context
                        SSLContext context = getSSLContext();
                        SSLEngine engine = context.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());

                        // Set the SSL parameters
                        SSLParameters sslParameters = context.getSupportedSSLParameters();
                        params.setSSLParameters(sslParameters);

                    } catch (Exception ex) {
                        System.out.println("Failed to create HTTPS port");
                    }
                }
            });
            //Map th httphandlers to url
            httpsServer.createContext("/inbox", new getMyMessage());
            httpsServer.createContext("/list", new listUsers());
            httpsServer.createContext("/register", new registerUser());
            httpsServer.createContext("/send", new sendMessages());
            httpsServer.createContext("/test", new MyHandler());
            httpsServer.setExecutor(null); // creates a default executor
            httpsServer.start();
            System.out.println("server started ");
        } catch (Exception exception) {
            System.out.println("Failed to create HTTPS server on port " + 8000 + " of localhost");
            exception.printStackTrace();

        }
    }
}
