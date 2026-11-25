// SPDX-FileCopyrightText: 2026 ARMSX2mil contributors
// SPDX-License-Identifier: GPL-3.0+

#pragma once

#include <string_view>

namespace HelioG99Profile
{
// MediaTek Helio G99 is commonly identified by its SoC code MT6789.
// Keep this profile scoped to the known Mali-G57 GPU; never force it on
// devices whose graphics renderer reports a different GPU.
inline constexpr std::string_view kSocId = "mt6789";
inline constexpr std::string_view kSocName = "helio g99";
inline constexpr char kGpuSeries = 'G';
inline constexpr unsigned short kGpuModel = 57;
inline constexpr unsigned char kGpuCoreCount = 2;
} // namespace HelioG99Profile
