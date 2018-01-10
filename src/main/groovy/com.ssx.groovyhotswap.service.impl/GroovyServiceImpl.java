package com.ssx.groovyhotswap.service.impl;

import com.ssx.groovyhotswap.service.GroovyService;
import org.springframework.stereotype.Service;

@Service
public class GroovyServiceImpl implements GroovyService {

    @Override
    public String hotswap() {
        return "hotswap-version-1.0";
    }

}
