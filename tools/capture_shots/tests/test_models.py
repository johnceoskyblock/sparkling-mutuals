import math
import unittest
from tools.capture_shots.models import Vec3,Sample,fit_track,predict,residual_stats

class ModelsTest(unittest.TestCase):
    def test_quadratic_fit_predicts_held_out_position_at_irregular_times(self):
        fit=fit_track([Sample(t,Vec3(2*t,3*t-.5*t*t,0)) for t in (0,.2,.7)],2)
        self.assertIsNotNone(fit)
        p=predict(fit,1);self.assertAlmostEqual(2,p.x,places=9);self.assertAlmostEqual(2.5,p.y,places=9)
        self.assertAlmostEqual(-1,fit.acceleration.y,places=9)
    def test_stationary_and_linear_traces_are_finite(self):
        for degree in (1,2):
            fit=fit_track([Sample(t,Vec3(2,3,4)) for t in (1,2,3,4)],degree)
            self.assertIsNotNone(fit);self.assertAlmostEqual(2,predict(fit,10).x)
        fit=fit_track([Sample(t,Vec3(2*t,-t,1)) for t in (0,1,3)],1)
        self.assertAlmostEqual(8,predict(fit,4).x)
    def test_insufficient_duplicate_nonfinite_missing_and_mixed_sources_are_unavailable(self):
        for samples in ([Sample(0,Vec3(0,0,0))], [Sample(0,Vec3(0,0,0))]*4,
                        [Sample(t,Vec3(math.inf,0,0)) for t in (0,1,2)],
                        [Sample(t,Vec3(t,0,0),base_known=False) for t in (0,1,2)],
                        [Sample(t,Vec3(t,0,0),source='tick' if t==1 else 'raw') for t in (0,1,2)]):
            self.assertIsNone(fit_track(samples,2))
    def test_residual_statistics_are_manually_checkable_and_last_is_not_maximum(self):
        stats=residual_stats([0,3,4,1])
        self.assertEqual(4,stats['count']);self.assertAlmostEqual(math.sqrt(6.5),stats['rmse'])
        self.assertEqual(1,stats['p50']);self.assertEqual(4,stats['p95']);self.assertEqual(4,stats['maximum'])
        self.assertEqual(1,stats['last_observed_error']);self.assertIsNone(residual_stats([])['rmse'])

if __name__=='__main__':unittest.main()
