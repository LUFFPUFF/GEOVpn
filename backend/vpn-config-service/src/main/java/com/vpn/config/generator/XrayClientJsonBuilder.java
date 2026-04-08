package com.vpn.config.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vpn.common.dto.ServerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class XrayClientJsonBuilder {
    private final ObjectMapper mapper = new ObjectMapper();

    public String buildClientJson(UUID uuid, ServerDto server, String serverName) {
        ObjectNode root = mapper.createObjectNode();

        root.put("remarks", serverName);

        ArrayNode outbounds = root.putArray("outbounds");

        ObjectNode proxy = outbounds.addObject();
        proxy.put("tag", "proxy");
        proxy.put("protocol", "vless");

        ObjectNode vnext = proxy.putObject("settings").putArray("vnext").addObject();
        vnext.put("address", server.getIpAddress()).put("port", server.getPort());
        vnext.putArray("users").addObject()
                .put("id", uuid.toString())
                .put("encryption", "none")
                .put("flow", "xtls-rprx-vision");

        ObjectNode stream = proxy.putObject("streamSettings");
        stream.put("network", "tcp").put("security", "reality");
        ObjectNode reality = stream.putObject("realitySettings");
        reality.put("serverName", server.getRealitySni() != null ? server.getRealitySni() : "eh.vk.com");
        reality.put("publicKey", server.getRealityPublicKey());
        reality.put("shortId", server.getRealityShortId());
        reality.put("fingerprint", "chrome");

        outbounds.addObject().put("tag", "direct").put("protocol", "freedom");
        outbounds.addObject().put("tag", "block").put("protocol", "blackhole");

        ObjectNode routing = root.putObject("routing");
        routing.put("domainStrategy", "IPIfNonMatch");
        ArrayNode rules = routing.putArray("rules");

        rules.addObject().put("type", "field").put("outboundTag", "block").putArray("domain").add("geosite:category-ads-all");

        ObjectNode ruRule = rules.addObject();
        ruRule.put("type", "field").put("outboundTag", "direct");
        ruRule.putArray("domain").add("geosite:category-ru").add("geosite:yandex").add("regexp:\\.ru$");
        ruRule.putArray("ip").add("geoip:ru").add("geoip:private");

        rules.addObject().put("type", "field").put("outboundTag", "proxy").put("network", "tcp,udp");

        return root.toString();
    }
}