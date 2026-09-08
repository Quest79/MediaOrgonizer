package com.quest79.mediaorganizer.metadata;

import com.quest79.mediaorganizer.model.MediaKind;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface MetadataProvider {

    String id();

    String displayName();

    Set<MediaKind> supportedKinds();

    CompletableFuture<List<MetadataCandidate>> search(MetadataQuery query);
}
