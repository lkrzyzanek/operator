package io.websitecd.operator.rest;

import io.quarkus.vertx.web.Route;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.websitecd.operator.webhook.WebhookService;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class WebHookResource {

    public static final String CONTEXT = "api/webhook/";

    private static final Logger log = Logger.getLogger(WebHookResource.class);

    @Inject
    WebhookService webhookService;

    public static List<String> apis(String rootPath) {
        List<String> apis = new ArrayList<>();
        apis.add(rootPath + CONTEXT);
        return apis;
    }

    @Route(methods = HttpMethod.POST, path = "/api/webhook", produces = "application/json")
    public void webhook(RoutingContext rc) {
        Buffer body = rc.getBody();
        HttpServerRequest request = rc.request();
        log.infof("webhook called from url=%s", request.remoteAddress());
        WebhookService.GIT_PROVIDER provider = webhookService.gitProvider(request);
        if (provider == null) {
            rc.response().setStatusCode(400).end("Unknown provider");
            return;
        }

        Future<JsonObject> result;
        if (provider.equals(WebhookService.GIT_PROVIDER.GITLAB)) {
            result = webhookService.handleGitlab(request, body);
        } else {
            rc.response().setStatusCode(400).end("Unknown provider");
            return;
        }
        result.onFailure(err -> rc.fail(err))
                .onSuccess(ar -> rc.response().end(ar.toBuffer()));
    }

}
