package com.vpn.config.dto.admin;

import com.vpn.config.domain.valueobject.StoredVpnLinks;
import lombok.Data;

import java.util.List;

@Data
public class AdminConfigUpdateRequest {
    private List<StoredVpnLinks.DirectLink> vlessLinks;
    private List<StoredVpnLinks.RelayLink> relayLinks;
    private List<String> hy2Links;
}
