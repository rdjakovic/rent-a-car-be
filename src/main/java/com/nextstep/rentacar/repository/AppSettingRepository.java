package com.nextstep.rentacar.repository;

import com.nextstep.rentacar.domain.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
