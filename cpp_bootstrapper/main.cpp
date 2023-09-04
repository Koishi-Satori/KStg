#pragma clang diagnostic push
#pragma ide diagnostic ignored "bugprone-misplaced-pointer-arithmetic-in-alloc"
#pragma GCC optimize("O3")
#pragma GCC optimize("Ofast,no-stack-protector,unroll-loops,fast-math")
#pragma GCC target( \
		"sse,sse2,sse3,ssse3,sse4.1,sse4.2,avx,avx2,popcnt,tune=native")

#include "bin/util.h"
#include <algorithm>
#include <cstdio>
#include <cstdlib>
#include <emmintrin.h>
#include <filesystem>
#include <immintrin.h>
#include <sys/stat.h>

#define EXIT_SYS_ERROR 1
#define return_dirt return EXIT_SYS_ERROR

static const char *JAR_SETTINGS = "./jar_settings.txt";
static const char *VM_OPTIONS = "./kstg.vmoptions";

int main(int argc, char **args) {
	// Find JRE.
	char *jre_c = getenv("Kkoishi_JDK");
	std::string jre;
	if (jre_c == nullptr)
		jre.assign("");
	else
		jre.assign(jre_c);
	std::string java_exe, exec, cur = kkoishi_kstg_boot::cwd(args[0]);
	if (jre.empty()) {
		jre.assign(getenv("JAVA_HOME"));
		if (jre.empty())
			jre.assign(getenv("JDK_HOME"));
	}

	java_exe = jre + "/bin/java";
	if (access(java_exe.c_str(), F_OK) == -1 &&
		access((java_exe + ".exe").c_str(), F_OK) == -1) {
		std::cout << "ERROR: Failed to start KStg Engine. Java: " << java_exe
				  << std::endl;
		std::cout << " No JRE is found, please define local variable pointed to "
					 "valid path"
				  << std::endl;
		std::cout << " Local Variables: Kkoishi_JDK, JAVA_HOME, JDK_HOME"
				  << std::endl;
		std::cout << "And there must exist ./bin/java.exe in the path."
				  << std::endl;
		return_dirt;
	}

	if (access((java_exe + "w").c_str(), F_OK) == -1 &&
		access((java_exe + "w.exe").c_str(), F_OK) == -1) {
		std::cout << "IGNORED ERROR: Failed to access javaw" << std::endl;
	} else
		java_exe += "w";

	exec = "\"" + java_exe + "\"";

	exec += kkoishi_kstg_boot::read_jvm_options(VM_OPTIONS);

	exec += " -cp ";
	kkoishi_kstg_boot::settings *_settings = kkoishi_kstg_boot::read_jar_settings(JAR_SETTINGS);
	if (_settings == nullptr) {
		std::cerr << "Failed to read JAR settings from " << JAR_SETTINGS << std::endl;
		return_dirt;
	}
	exec += _settings->class_path;
	exec += ' ';
	exec += _settings->main_class;

	if (argc > 1)
		exec += kkoishi_kstg_boot::processArguments(1, argc, args);
	for (const auto &argument : _settings->program_arguments) {
		exec += ' ';
		exec += argument;
	}

	std::cout << exec << std::endl;
	auto ps = popen(exec.c_str(), "w");
	return pclose(ps);
}
