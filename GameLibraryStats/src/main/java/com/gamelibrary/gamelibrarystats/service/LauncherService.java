package com.gamelibrary.gamelibrarystats.service;

import com.gamelibrary.gamelibrarystats.model.Launcher;
import com.gamelibrary.gamelibrarystats.repository.LauncherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LauncherService {
    @Autowired
    private LauncherRepository launcherRepository;

    public List<Launcher> getAllLaunchers() {
        return launcherRepository.findAll();
    }


}