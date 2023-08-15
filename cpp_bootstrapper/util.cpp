//
// Created by KKoishi_ on 2023/8/15.
//

#include <immintrin.h>
#include <emmintrin.h>
#include <bits/stdc++.h>
#include "filesystem"
#include <sys/stat.h>
#include <algorithm>
namespace kkoishi_kstg_boot {
    std::string cwd(const char *exep) {
        std::string res, buf;
        const auto l = strlen(exep);
        for (int i = 0; i < l; ++i) {
            const auto c = exep[i];
            buf.push_back(c);
            if ((c == '\\' || c == '/') && !buf.empty()) {
                res += buf;
                buf = "";
            }
        }
        return res;
    }
}

