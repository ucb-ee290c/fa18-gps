#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

export HAMMER_HOME=$DIR/hammer

source $HAMMER_HOME/sourceme.sh

cd "$DIR" && hammer-vlsi synthesis -o syn-output.json -v verisim/generated-src/freechips.rocketchip.system.DefaultConfig.v --top ExampleTopWithTracking -p config/clocks.json -p config/use_vivado.json  --obj_dir out
