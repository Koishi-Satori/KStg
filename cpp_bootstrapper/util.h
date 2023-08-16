//
// Created by KKoishi_ on 2023/8/15.
//

#ifndef CPP_BOOTSTRAPPER_UTIL_H
#define CPP_BOOTSTRAPPER_UTIL_H

namespace kkoishi_kstg_boot {
    std::string cwd(const char *exep);
    std::string read_jvm_options(const char *file_name);
    std::string processArguments(int begin, int argc, char **args);
}

#endif //CPP_BOOTSTRAPPER_UTIL_H
