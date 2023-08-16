//
// Created by KKoishi_ on 2023/8/15.
//

#include <immintrin.h>
#include <emmintrin.h>
#include <bits/stdc++.h>
#include "filesystem"
#include <sys/stat.h>
#include <algorithm>
#include <utility>

#define default_vm_option " -Dsun.java2d.ddscale=true -Dsun.java2d.opengl=true -Dswing.aatext=true -Dawt.nativeDoubleBuffering=true "

namespace kkoishi_kstg_boot {

    namespace inner {
        template<typename return_type, typename option_type>
        class option {
        private:
            option_type option_name;

            return_type (*func)(option_type);

        public:
            bool has_arg;

            option(bool hasArg, return_type (*func)(option_type), option_type option_name) : has_arg(hasArg),
                                                                                             func(func),
                                                                                             option_name(std::move(
                                                                                                     option_name)) {}

            bool check(option_type arg) {
                return arg == option_name;
            }

            return_type invoke(option_type parameter) {
                return func(parameter);
            }
        };

        using ss_option = inner::option<std::string, std::string>;
    }

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

    std::string read_jvm_options(const char *file_name) {
        std::ifstream ifs;
        ifs.open(file_name, std::ios::in);

        if (!ifs.is_open()) {
            std::cout << "IGNORED ERROR: Failed to open the file: " << file_name << std::endl;
            return default_vm_option;
        }

        std::string res(" "), buf;
        std::vector<std::string> options;
        while (getline(ifs, buf)) {
            options.push_back(buf);
        }
        for (const auto &str: options) {
            res += (str + " ");
        }

        return res;
    }


    std::string processArguments(int begin, int argc, char **args) {
        if (begin == argc)
            return "";
        std::string res;
        std::vector<inner::ss_option> options = {
                inner::ss_option(false, [](std::string parameter) -> std::string { return parameter.erase(0, 1); },
                                 std::string("-fullscreen"))};
        for (int i = begin; i < argc; ++i) {
            res += " ";
            const char *arg = args[i];
            for (auto &item: options) {
                if (item.check(arg)) {
                    if (item.has_arg && ++i < argc) {
                        const char *next = args[i];
                        res += (item.invoke(next));
                    } else
                        res += (item.invoke(arg));
                }
            }
        }

        return res;
    }
}

