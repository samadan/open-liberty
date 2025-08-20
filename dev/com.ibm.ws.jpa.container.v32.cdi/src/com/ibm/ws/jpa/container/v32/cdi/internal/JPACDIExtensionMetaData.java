package com.ibm.ws.jpa.container.v32.cdi.internal;

import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.extension.CDIExtensionMetadataInternal;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import jakarta.enterprise.inject.spi.Extension;

@Component(service = CDIExtensionMetadata.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true)
public class JPACDIExtensionMetaData implements CDIExtensionMetadata, CDIExtensionMetadataInternal {

    //Needed to reference qualifiers defined in the application
    @Override
    public boolean applicationBeansVisible() {
        return true;
    }

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        Set<Class<? extends Extension>> extensions = new HashSet<Class<? extends Extension>>();
        extensions.add(JPACDIExtension.class);
        return extensions;
    }
}
