# include "position.h"
# include "matrix_math.h"
# include <stdio.h>
# include <stdlib.h>
# include <math.h>

//Documented in doc/Ranging.md

//calculates pseudorange from nominal location to known satellite location
//by using the current locations and the nominal time bias and the speed of light

double calc_pseudorange(struct double_sat_loc_params* sat, double nom[4]) {
  return sqrt(powl(nom[0] - (sat->Xx), 2.0) + pow(nom[1] - (sat->Yy), 2.0) + pow(nom[2] - (sat->Zz), 2.0)) - nom[3]*c;
}

void find_position(struct double_sat_loc_params* sv_1, struct double_sat_loc_params* sv_2, struct double_sat_loc_params* sv_3, struct double_sat_loc_params* sv_4, struct delta_t* time_deltas, struct ecef_pos* position) {

  double sv_data[4][4];
  sv_data[0][0] = sv_1->Xx;   
  sv_data[0][1] = sv_1->Yy;   
  sv_data[0][2] = sv_1->Zz;   
  sv_data[0][3] = time_deltas->delta_t_1;   
  sv_data[1][0] = sv_2->Xx;   
  sv_data[1][1] = sv_2->Yy;   
  sv_data[1][2] = sv_2->Zz;   
  sv_data[1][3] = time_deltas->delta_t_2;   
  sv_data[2][0] = sv_3->Xx;   
  sv_data[2][1] = sv_3->Yy;   
  sv_data[2][2] = sv_3->Zz;   
  sv_data[2][3] = time_deltas->delta_t_3;   
  sv_data[3][0] = sv_4->Xx;   
  sv_data[3][1] = sv_4->Yy;   
  sv_data[3][2] = sv_4->Zz;   
  sv_data[3][3] = time_deltas->delta_t_4;   
  
  double nom[4] = {0.0, 0.0, 0.0, 0.0};
  double pr_nom[4];
  double deltas[4] = {0.0, 0.0, 0.0, 0.0};
  double delta_pr[4];
  double alpha[4][4];
  double alpha_inv[4][4];
  double alpha_flat[16];
  double alpha_flat_inv[16];
  double error_mag = 1000.0;

  for (int h=0; h<10; h++) {
    nom[0] = nom[0] + deltas[0];
    nom[1] = nom[1] + deltas[1];
    nom[2] = nom[2] + deltas[2];
    nom[3] = nom[3] + deltas[3];
    printf("nom[0]: %f\n", nom[0]); 
    printf("nom[1]: %f\n", nom[1]);
    printf("nom[2]: %f\n", nom[2]);
    printf("nom[3]: %f\n", nom[3]);

    pr_nom[0] = calc_pseudorange(sv_1, nom);
    pr_nom[1] = calc_pseudorange(sv_2, nom);
    pr_nom[2] = calc_pseudorange(sv_3, nom);
    pr_nom[3] = calc_pseudorange(sv_4, nom); 
    printf("pr_nom[0]: %f\n", pr_nom[0]);
    printf("pr_nom[1]: %f\n", pr_nom[1]);
    printf("pr_nom[2]: %f\n", pr_nom[2]);
    printf("pr_nom[3]: %f\n", pr_nom[3]);

    delta_pr[0] = c*time_deltas->delta_t_1 - pr_nom[0]; 
    delta_pr[1] = c*time_deltas->delta_t_2 - pr_nom[1];
    delta_pr[2] = c*time_deltas->delta_t_3 - pr_nom[2];
    delta_pr[3] = c*time_deltas->delta_t_4 - pr_nom[3];    
    printf("delta_pr[0]: %f\n", delta_pr[0]);
    printf("delta_pr[1]: %f\n", delta_pr[1]);
    printf("delta_pr[2]: %f\n", delta_pr[2]);
    printf("selta_pr[3]: %f\n", delta_pr[3]);

    for (int i=0; i<4; i++) {
      for (int j=0; j<4; j++) {
        if (j == 3) {
	  alpha[i][j] = c;
	  alpha_flat[i*4 + j] = c;
	} else {
          alpha[i][j] = (nom[j] - sv_data[i][j]) / (pr_nom[i] - nom[3]*c); 	
          alpha_flat[i*4 + j] = (nom[j] - sv_data[i][j]) / (pr_nom[i] - nom[3]*c); 
	}
      }
    }

   matrix_inverse(alpha_flat, alpha_flat_inv);
   mat_vec_dot(alpha_flat_inv, delta_pr, deltas);
   printf("deltas[0]: %f\n", deltas[0]);
   printf("deltas[1]: %f\n", deltas[1]);
   printf("deltas[2]: %f\n", deltas[2]);
   printf("deltas[3]: %f\n", deltas[3]);
   
   error_mag = sqrt(pow(deltas[0], 2.0) + pow(deltas[1], 2.0) + pow(deltas[2], 2.0));
   printf("Error Mag: %f\n", error_mag);
  }

  position->x = nom[0];
  position->y = nom[1];
  position->z = nom[2];

}

int main() {
  struct double_sat_loc_params sv_1 = { 0.0, -20200000.0-3894033.0, 0.0 };  
  struct double_sat_loc_params sv_2 = {20200000.0+3894033.0, 0.0, 0.0};
  struct double_sat_loc_params sv_3 = {-20200000.0-3894033.0, 0.0, 0.0};
  struct double_sat_loc_params sv_4 = {0.0, 0.0, 20200000.0+3894033.0};


  struct ecef_pos final_result;
  struct delta_t time_deltas = {0.06796112069, 0.09133565296, 0.07389219269, 0.069398661109};

  find_position(&sv_1, &sv_2, &sv_3, &sv_4, &time_deltas, &final_result);

  return 0;
}
