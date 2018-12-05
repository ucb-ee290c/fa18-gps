close all;

n_freq_half = 20; 
freq_carrier = 4127190;
n_freq = n_freq_half*2+1; 

freq_step = 500; 
freq_start = freq_carrier - n_freq_half * freq_step;
freq_stop = freq_carrier + n_freq_half * freq_step;

n_chip = 2046; 
chip_step = 8;

corr_list = importdata('corrArr_41p.txt');
corr_arr = reshape(corr_list, n_chip, n_freq);

[X, Y] = meshgrid((freq_start:freq_step:freq_stop)/1e6, 0:chip_step:(n_chip-1)*chip_step);
% [X, Y] = meshgrid(0:8:(n_freq-1)*8, 0:chip_step:(n_chip-1)*chip_step);

% surfc(X, Y, corr_arr, gradient(corr_arr));
surfc(X, Y, corr_arr);
colormap parula;
colorbar;

set(gca, 'FontSize', 16, 'FontWeight', 'bold')
zlabel('Correlation', 'FontSize', 16, 'FontWeight', 'bold', 'Color', 'k')
xlabel('Frequency (MHz)', 'FontSize', 16, 'FontWeight', 'bold', 'Color', 'k')
ylabel('Chip Phase', 'FontSize', 16, 'FontWeight', 'bold', 'Color', 'k')
