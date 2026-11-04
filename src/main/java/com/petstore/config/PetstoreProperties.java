package com.petstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

@ConfigurationProperties("petstore")
public record PetstoreProperties(@DefaultValue Populate populate, @DefaultValue Paging paging) {

    /**
     * @param allowForce whether the landing page may wipe and reload the data. Fine for a demo, off in prod.
     */
    public record Populate(@DefaultValue("true") boolean onStartup,
                           @DefaultValue("false") boolean allowForce,
                           @DefaultValue("classpath:seed/Populate-UTF8.xml") Resource seedFile) {
    }

    /**
     * Legacy page sizes: 2 rows on category/product/search pages, 10 in My List. Two is tiny, but changing
     * it changes every paging link, so it stays at 2 until someone decides otherwise.
     */
    public record Paging(@DefaultValue("2") int listSize,
                         @DefaultValue("10") int myListSize) {
    }
}
