package com.vpn.server.service;

import com.vpn.common.dto.ServerDto;
import com.vpn.common.exception.BaseException;
import com.vpn.common.constant.ErrorCode;
import com.vpn.server.domain.entity.Server;
import com.vpn.server.dto.mapper.ServerMapper;
import com.vpn.server.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerService {

    private final ServerRepository serverRepository;
    private final ServerMapper serverMapper;

    public List<ServerDto> getAllActiveServers() {
        return serverMapper.toDtoList(serverRepository.findByIsActiveTrue());
    }

    public ServerDto getServerById(Integer id) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.SERVER_NOT_FOUND, "Server not found: " + id));
        return serverMapper.toDto(server);
    }
}
