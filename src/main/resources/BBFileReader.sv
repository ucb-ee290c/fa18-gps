module BBFileReader #(
   parameter IO_READWIDTH = 8,
   parameter FILE_NAME = "") (
  input clk,
  input data_run,
  output data_valid,
  output reg [IO_READWIDTH-1:0] data_out 
);

  integer data_file; //file handler
  integer scan_file; //file handler
  reg [IO_READWIDTH-1:0] input_data;
  `define NULL 0

  initial begin
     data_file = $fopen(FILE_NAME, "rb");
     if (data_file == `NULL) begin
       $display("data_file handle was NULL");
       $finish;
     end
  end

  always @(posedge clk) begin
     if (data_run) begin
       scan_file = $fscanf(data_file, "%d\n", input_data);
       if (!$feof(data_file)) begin
         data_out <= input_data;
       end
     end
     data_valid <= data_run & !$feof(data_file);
  end

endmodule
