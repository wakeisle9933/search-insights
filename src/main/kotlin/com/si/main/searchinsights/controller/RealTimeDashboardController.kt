package com.si.main.searchinsights.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class RealTimeDashboardController {

    @GetMapping("/dashboard")
    fun dashboard(): String {
        return "realtime-dashboard" // templates/realtime-dashboard.html 을 찾아줌!
    }

}