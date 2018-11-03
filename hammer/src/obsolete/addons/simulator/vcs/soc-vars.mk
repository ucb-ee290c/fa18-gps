# Copyright 2016 Palmer Dabbelt <palmer@dabbelt.com>

include $(SOC_SIMULATOR_ADDON)/_vars.mk

ifeq ($(filter $(MAKECMDGOALS),clean distclean),)
-include $(OBJ_SOC_DIR)/vcs-vars.mk
$(OBJ_SOC_DIR)/vcs-vars.mk: $(SOC_SIMULATOR_ADDON)/tools/generate-vars
	mkdir -p $(dir $@)
	$< --output $@ --upper SOC --lower soc --vtype RTL
endif
