package org.rapidoid.websocket;

import org.rapidoid.http.MediaType;
import org.rapidoid.http.HttpStatus;
import org.rapidoid.http.Req;
import org.rapidoid.http.Route;
import org.rapidoid.http.handler.HttpHandler;
import org.rapidoid.http.impl.RouteOptions;
import org.rapidoid.lambda.TwoParamLambda;
import org.rapidoid.net.abstracts.Channel;
import org.rapidoid.net.abstracts.IRequest;

import java.util.Map;

/**
 * Created by randy on 7/27/16.
 */
public class WebSocketHandler implements HttpHandler {

    IWebSocketRequest req;
    RouteOptions options;
    IWebSocketProtocol proto;
    Route route;
    public WebSocketHandler(IWebSocketProtocol proto, IWebSocketRequest req, RouteOptions opt) {
        this.req = req;
        this.options = opt;
        this.proto = proto;
    }

    @Override
    public HttpStatus handle(Channel ctx, boolean isKeepAlive, Req req, Object extra) {
        return HttpStatus.ASYNC;
    }

    @Override
    public boolean needsParams() {
        return true;
    }

    @Override
    public MediaType contentType() {
        return MediaType.HTML_UTF_8;
    }

    @Override
    public RouteOptions options() {
        return options;
    }

    @Override
    public void setRoute(Route route) {
        this.route = route;
    }

    @Override
    public HttpHandler getHandler() {
        return this;
    }

    @Override
    public Map<String, String> getParams() {
        return null;
    }

    @Override
    public Route getRoute() {
        return route;
    }

    public void setOptions(RouteOptions opts){
        this.options = opts;
    }

    public void handleRequest(IWebSocketProtocol proto, Req httpReq, IWebSocketConnection conn){
        req.onRequest(proto, httpReq, conn);
    }
}
