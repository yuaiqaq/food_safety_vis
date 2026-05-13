package com.foodsafety.service;

import com.foodsafety.entity.NodeLayout;
import com.foodsafety.repository.NodeLayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LayoutService {

    private final NodeLayoutRepository nodeLayoutRepository;

    // ✅ 方法名改成和你调用的一样：getByFilterKey
    public List<NodeLayout> getByFilterKey(String filterKey) {
        return nodeLayoutRepository.findByFilterKey(filterKey);
    }
}