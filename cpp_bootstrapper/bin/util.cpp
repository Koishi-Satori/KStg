//
// Created by KKoishi_ on 2023/8/15.
//
#pragma clang diagnostic push
#pragma ide diagnostic ignored "bugprone-misplaced-pointer-arithmetic-in-alloc"
#pragma GCC optimize("O3")
#pragma GCC optimize("Ofast,no-stack-protector,unroll-loops,fast-math")
#pragma GCC target( \
		"sse,sse2,sse3,ssse3,sse4.1,sse4.2,avx,avx2,popcnt,tune=native")

#include <algorithm>
#include <fstream>
#include <ios>
#include <iostream>
#include <map>
#include <string>
#include <sys/stat.h>
#include <unordered_map>
#include <utility>
#include <vector>

#define default_vm_option                                                      \
	" -Dsun.java2d.ddscale=true -Dsun.java2d.opengl=true -Dswing.aatext=true " \
	"-Dawt.nativeDoubleBuffering=true "

namespace kkoishi_kstg_boot {

	namespace inner {
		template<typename return_type, typename option_type>
		class option {
		private:
			option_type option_name;

			return_type (*func)(option_type);

		public:
			bool has_arg;

			option(bool hasArg, return_type (*func)(option_type), option_type option_name)
				: has_arg(hasArg), func(func), option_name(std::move(option_name)) {}

			bool check(option_type arg) { return arg == option_name; }

			return_type invoke(option_type parameter) { return func(parameter); }
		};

		using ss_option = inner::option<std::string, std::string>;
	}// namespace inner

	static const std::string settings_keys[3] = {"class_path", "main_class", "default_args"};

	std::vector<std::string> get_args(const std::string &program_args);

	class settings {
	public:
		std::string class_path;
		std::string main_class;
		std::vector<std::string> program_arguments;

		settings(std::string class_path, std::string main_class,
				 const std::string &program_args)
			: class_path(std::move(class_path)), main_class(std::move(main_class)),
			  program_arguments(get_args(program_args)) {}
	};

	std::vector<std::string> get_args(const std::string &program_args) {
		if (program_args.empty())
			return {};
		bool index_str = false;
		std::vector<std::string> result;
		std::string buf;
		for (const auto &item : program_args) {
			if (item == '"') {
				index_str = !index_str;
				buf.push_back('"');
			} else if (!index_str && item == ';') {
				result.push_back(buf);
				buf.clear();
			} else
				buf.push_back(item);
		}
		if (!buf.empty())
			result.push_back(buf);
		return result;
	}

	std::string cwd(const char *work_dir) {
		std::string res, buf;
		const auto l = strlen(work_dir);
		for (int i = 0; i < l; ++i) {
			const auto c = work_dir[i];
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
			std::cout << "IGNORED ERROR: Failed to open the file: " << file_name
					  << std::endl;
			return default_vm_option;
		}

		std::string res(" "), buf;
		std::vector<std::string> options;
		while (getline(ifs, buf)) {
			options.push_back(buf);
		}
		for (const auto &str : options) {
			res += (str + " ");
		}

		return res;
	}

	std::string processArguments(int begin, int argc, char **args) {
		if (begin == argc)
			return "";
		std::string res;
		std::vector<inner::ss_option> options = {inner::ss_option(
				false,
				[](std::string parameter) -> std::string {
					return parameter.erase(0, 1);
				},
				std::string("-fullscreen"))};
		for (int i = begin; i < argc; ++i) {
			res += " ";
			const char *arg = args[i];
			for (auto &item : options) {
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

	settings *read_jar_settings(const char *path) {
		std::ifstream ifs;
		ifs.open(path, std::ios::in);
		if (!ifs.is_open()) {
			std::cerr << "Can not open the file " << path << std::endl;
			return nullptr;
		}

		std::unordered_map<std::string, std::string> settings_map;
		bool index_key = true;
		std::string key, value;
		char buf[1024] = {0};
		while (ifs.getline(buf, sizeof(buf))) {
			for (const auto &c : buf) {
				if (c == '\0' || c == '\n' || c == '\r')
					break;
				if (c == '=') {
					index_key = false;
					continue;
				}
				if (index_key)
					key.push_back(c);
				else
					value.push_back(c);
			}

			index_key = true;
			if (!key.empty() && !value.empty()) {
				settings_map.insert(std::pair<std::string, std::string>(key, value));
				key.clear();
				value.clear();
			}
		}

		std::string init_args[3] = {""};
		for (int i = 0; i < 3; ++i) {
			const auto &_key = settings_keys[i];
			auto iter = settings_map.find(_key);
			if (iter != settings_map.end())
				init_args[i] = iter->second;
		}

		static settings set(init_args[0], init_args[1], init_args[2]);
		return &set;
	}
}// namespace kkoishi_kstg_boot
