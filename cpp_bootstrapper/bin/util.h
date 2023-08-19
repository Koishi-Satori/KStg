//
// Created by KKoishi_ on 2023/8/15.
//

#ifndef CPP_BOOTSTRAPPER_UTIL_H
#define CPP_BOOTSTRAPPER_UTIL_H

#include <bits/stdc++.h>

namespace kkoishi_kstg_boot {
	class settings {
	public:
		std::string class_path;
		std::string main_class;
		std::vector<std::string> program_arguments;
	};

	std::string cwd(const char *work_dir);
	std::string read_jvm_options(const char *file_name);
	std::string processArguments(int begin, int argc, char **args);
	settings* read_jar_settings(const char *path);
}

#endif//CPP_BOOTSTRAPPER_UTIL_H
