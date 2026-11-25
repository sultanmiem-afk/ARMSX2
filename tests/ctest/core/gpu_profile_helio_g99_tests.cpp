// SPDX-FileCopyrightText: 2026 ARMSX2mil contributors
// SPDX-License-Identifier: GPL-3.0+

#include "GS/Renderers/Common/GSGPUProfile.h"

#include <gtest/gtest.h>

TEST(GpuProfileHelioG99Tests, InfersDualCoreMaliG57FromMt6789)
{
	MobileDriverContext context;
	context.platform_hints = "ro.soc.model=MT6789";

	const GpuProfileSelection selection =
		GpuProfileDetector::Resolve("auto", "ARM", "Mali-G57", context);

	EXPECT_EQ(selection.runtime_profile, RuntimeGpuProfile::Mali);
	EXPECT_EQ(selection.gpu.architecture, MobileGpuArchitecture::MaliValhall1);
	EXPECT_EQ(selection.gpu.model_number, 57);
	EXPECT_EQ(selection.gpu.core_count, 2);
	EXPECT_EQ(selection.gpu.name, "Mali-G57 MC2");
	EXPECT_TRUE(selection.gpu.recognized);

	// G57's normal tuning is capped by the inferred two-core configuration.
	EXPECT_EQ(selection.gs_tuning.pooled_targets, 64);
	EXPECT_EQ(selection.gs_tuning.pooled_textures, 64);
	EXPECT_EQ(selection.gs_tuning.target_age, 5);
	EXPECT_EQ(selection.gs_tuning.texture_age, 5);
	EXPECT_TRUE(selection.gs_tuning.constrained);
}

TEST(GpuProfileHelioG99Tests, DoesNotInferCoresForAnotherSoc)
{
	MobileDriverContext context;
	context.platform_hints = "ro.soc.model=MT6785";

	const GpuProfileSelection selection =
		GpuProfileDetector::Resolve("auto", "ARM", "Mali-G57", context);

	EXPECT_EQ(selection.gpu.core_count, 0);
	EXPECT_EQ(selection.gpu.name, "Mali-G57");
}

TEST(GpuProfileHelioG99Tests, DoesNotApplyHelioProfileToAnotherMaliModel)
{
	MobileDriverContext context;
	context.platform_hints = "ro.soc.model=MT6789";

	const GpuProfileSelection selection =
		GpuProfileDetector::Resolve("auto", "ARM", "Mali-G68", context);

	EXPECT_EQ(selection.gpu.core_count, 0);
	EXPECT_EQ(selection.gpu.model_number, 68);
	EXPECT_EQ(selection.gpu.name, "Mali-G68");
}

TEST(GpuProfileHelioG99Tests, PreservesAnExplicitlyReportedCoreCount)
{
	MobileDriverContext context;
	context.platform_hints = "ro.soc.model=MT6789";

	const GpuProfileSelection selection =
		GpuProfileDetector::Resolve("auto", "ARM", "Mali-G57 MC4", context);

	EXPECT_EQ(selection.gpu.core_count, 4);
	EXPECT_EQ(selection.gpu.name, "Mali-G57 MC4");
	EXPECT_EQ(selection.gs_tuning.pooled_targets, 72);
	EXPECT_EQ(selection.gs_tuning.pooled_textures, 72);
}
