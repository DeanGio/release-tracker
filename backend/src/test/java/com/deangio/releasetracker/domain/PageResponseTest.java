package com.deangio.releasetracker.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.deangio.releasetracker.api.PageResponse;

public class PageResponseTest {
    @Test
    void pageResponseComputesNextOffset() {
        var middle = PageResponse.of(List.of("a", "b"), 2, 0, 5);
        assertThat(middle.nextOffset()).isEqualTo(2);

        var last = PageResponse.of(List.of("e"), 2, 4, 5);
        assertThat(last.nextOffset()).isNull();
    }

}
